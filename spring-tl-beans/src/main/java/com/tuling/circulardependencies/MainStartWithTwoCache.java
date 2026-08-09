package com.tuling.circulardependencies;

import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 只使用一级缓存和二级缓存解决循环依赖（不考虑AOP）
 * 一级缓存 singletonObjects：存放完整的成熟态Bean
 * 二级缓存 earlySingletonObjects：存放实例化但未初始化的纯净态Bean
 */
public class MainStartWithTwoCache {

	public static void main(String[] args) throws Exception {
		String beanName = "com.tuling.circulardependencies.InstanceA";

		InstanceA a = (InstanceA) getBean(beanName);

		a.say();
	}

	// 一级缓存 单例池 存放成熟态Bean（完成属性赋值的完整Bean）
	private static Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

	// 二级缓存 存放纯净态Bean（实例化完成但未完成属性赋值的不完整Bean）
	// 同时也起到标识正在创建的作用：只要Bean在二级缓存中，就说明它尚未完成创建
	private static Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(256);

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

		// 1. 实例化
		Object beanInstance = beanClass.newInstance();

		// 实例化后直接放入二级缓存（不考虑AOP，无需三级缓存）
		earlySingletonObjects.put(beanName, beanInstance);

		// 2. 属性赋值 解析@Autowired
		Field[] declaredFields = beanClass.getDeclaredFields();
		for (Field declaredField : declaredFields) {
			Autowired annotation = declaredField.getAnnotation(Autowired.class);
			if (annotation != null) {
				Class<?> type = declaredField.getType();
				// 递归获取依赖的Bean
				Object fieldBean = getBean(type.getName());
				// 反射设置属性值
				declaredField.setAccessible(true);
				declaredField.set(beanInstance, fieldBean);
			}
		}

		// 3. 初始化（省略）

		// 从二级缓存移除，放入一级缓存
		earlySingletonObjects.remove(beanName);
		singletonObjects.put(beanName, beanInstance);

		return beanInstance;
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
		// 只要Bean在二级缓存中，就说明它正在创建中（实例化但未完成属性赋值），即发生了循环依赖
		bean = earlySingletonObjects.get(beanName);
		return bean;
	}
}
