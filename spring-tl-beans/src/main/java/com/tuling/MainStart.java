package com.tuling;

import com.tuling.iocbeanlifecicle.Car;
import com.tuling.iocbeanlifecicle.MainConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by xsls on 2019/7/7.
 */
public class MainStart {

	public static void main(String[] args) {
//		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:/beans/beans.xml");
//		ctx.getBean("car");
		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(MainConfig.class);
		Car car = (Car) annotationConfigApplicationContext.getBean("car");
		System.out.println(car.getName());
		/*System.out.println(ctx.getBean(TulingDao.class).getTulingDataSource().getFlag());*/
	}

}
