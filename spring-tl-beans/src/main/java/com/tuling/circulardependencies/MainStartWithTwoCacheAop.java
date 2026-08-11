package com.tuling.circulardependencies;

import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 只使用一级缓存和二级缓存解决循环依赖（包含AOP）
 *
 * 与三级缓存方案的区别：
 *   三级缓存通过 ObjectFactory 延迟创建AOP代理，只有发生循环依赖时才创建代理；
 *   二级缓存方案在实例化后立即创建AOP代理并放入二级缓存，不管是否发生循环依赖都会提前创建代理。
 *   这就是 Spring 用三级缓存的原因：避免对不需要循环依赖的Bean提前创建代理。
 *
 * 一级缓存 singletonObjects：存放完整的成熟态Bean（可能是代理对象）
 * 二级缓存 earlySingletonObjects：存放实例化后的代理对象（未完成属性赋值）
 */
public class MainStartWithTwoCacheAop {

	public static void main(String[] args) throws Exception {
		String beanName = "com.tuling.circulardependencies.InstanceA";

		IApi a = (IApi) getBean(beanName);

		a.say();
	}

	// 一级缓存 单例池 存放成熟态Bean（完成属性赋值的完整Bean，可能是代理）
	private static Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

	// 二级缓存 存放实例化后的代理对象（未完成属性赋值的不完整Bean）
	// 只要Bean在二级缓存中，就说明它正在创建中
	private static Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(256);

	// AOP后置处理器
	private static JdkProxyBeanPostProcessor beanPostProcessor = new JdkProxyBeanPostProcessor();

	/**
	 * 获取Bean
	 */
	private static Object getBean(String beanName) throws Exception {
		Class<?> beanClass = Class.forName(beanName);

		// 先从缓存中获取
		Object bean = getSingleton(beanName);
		if (bean != null) {
			return bean;
		}

		// 1. 实例化（原始对象）
		Object beanInstance = beanClass.newInstance();

		// 实例化后立即调用AOP后置处理器，可能返回代理对象
		// 二级缓存方案无法延迟创建代理，必须在这里提前创建
		Object earlyBeanReference = beanPostProcessor.getEarlyBeanReference(beanInstance, beanName);

		// 将代理对象（或原始对象，如果没有被增强）放入二级缓存
		earlySingletonObjects.put(beanName, earlyBeanReference);

		// 2. 属性赋值 解析@Autowired
		// 注意：属性赋值在原始对象上进行，代理对象持有原始对象引用，会委托调用
		Field[] declaredFields = beanClass.getDeclaredFields();
		for (Field declaredField : declaredFields) {
			Autowired annotation = declaredField.getAnnotation(Autowired.class);
			if (annotation != null) {
				Class<?> type = declaredField.getType();
				// 递归获取依赖的Bean
				Object fieldBean = getBean(type.getName());
				// 反射设置属性值到原始对象上
				declaredField.setAccessible(true);
				declaredField.set(beanInstance, fieldBean);
			}
		}

		// 3. 初始化（省略）

		// 从二级缓存取出代理对象，放入一级缓存
		// 最终存入一级缓存的是代理对象，保证整个容器中是同一个代理
		earlySingletonObjects.remove(beanName);
		singletonObjects.put(beanName, earlyBeanReference);

		return earlyBeanReference;
	}

	/**
	 * 从缓存中获取Bean
	 * 先查一级缓存，没有再查二级缓存
	 */
	private static Object getSingleton(String beanName) {
		// 先从一级缓存获取（成熟态Bean）
		Object bean = singletonObjects.get(beanName);
		if (bean != null) {
			return bean;
		}

		// 一级缓存没有，直接查二级缓存
		// 只要Bean在二级缓存中，就说明它正在创建中，即发生了循环依赖
		// 返回的是代理对象，保证循环依赖注入的也是代理
		bean = earlySingletonObjects.get(beanName);
		return bean;
	}
}
