package wxdgaming.boot2.core;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.reflect.ReflectContext;

import java.lang.annotation.Annotation;

/**
 * 基础模块
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2023-09-15 10:12
 **/
@Slf4j
@Getter
public abstract class BaseModule extends AbstractModule {

    final ReflectContext reflectContext;

    public BaseModule(ReflectContext reflectContext) {
        this.reflectContext = reflectContext;
    }

    /** 绑定指定注解 */
    public void bindClassWithAnnotated(Class<? extends Annotation> annotation) {
        reflectContext.classWithAnnotated(annotation)
                .sorted(ReflectContext.ComparatorClassBySort)
                .forEach(this::bindSingleton);
    }

    public void bindSingleton(Class<?> clazz) {
        log.debug("bind clazz {} {} clazz={}", this.getClass().getName(), this.hashCode(), clazz);
        bind(clazz).in(Singleton.class);
    }

    public <R> void bindSingleton(Class<R> father, Class<? extends R> son) {
        log.debug("bind clazz father to son {} {} father={} son={}", this.getClass().getName(), this.hashCode(), father, son);
        bind(father).to(son).in(Singleton.class);
    }

    public <B> void bindSingleton(Class<B> clazz, B instance) {
        log.debug("bind instance {} {} clazz={} instance={}", this.getClass().getName(), this.hashCode(), clazz, instance.getClass());
        bind(clazz).toInstance(instance);
    }

    @Override
    protected void configure() {
        binder().requireExplicitBindings();
        binder().requireExactBindingAnnotations();
        // binder().disableCircularProxies();/*禁用循环依赖*/

        try {
            bind();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void bind() throws Throwable;

}
