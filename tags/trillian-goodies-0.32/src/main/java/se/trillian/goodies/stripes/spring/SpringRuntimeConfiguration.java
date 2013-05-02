/*
 * Copyright (c) 2004-2008, Trillian AB. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.trillian.goodies.stripes.spring;

import java.util.Collection;
import java.util.Map;

import net.sourceforge.stripes.config.ConfigurableComponent;
import net.sourceforge.stripes.config.DefaultConfiguration;
import net.sourceforge.stripes.config.RuntimeConfiguration;
import net.sourceforge.stripes.controller.ActionBeanContextFactory;
import net.sourceforge.stripes.controller.ActionBeanPropertyBinder;
import net.sourceforge.stripes.controller.ActionResolver;
import net.sourceforge.stripes.controller.Interceptor;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.controller.multipart.MultipartWrapperFactory;
import net.sourceforge.stripes.exception.ExceptionHandler;
import net.sourceforge.stripes.exception.StripesRuntimeException;
import net.sourceforge.stripes.format.FormatterFactory;
import net.sourceforge.stripes.localization.LocalePicker;
import net.sourceforge.stripes.localization.LocalizationBundleFactory;
import net.sourceforge.stripes.tag.PopulationStrategy;
import net.sourceforge.stripes.tag.TagErrorRendererFactory;
import net.sourceforge.stripes.validation.TypeConverterFactory;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * This class provides a way to configure Stripes using the Spring application
 * instead of defining filter parameters in web.xml. In order to use this
 * instead of the standard {@link RuntimeConfiguration} set the filter
 * init-param <code>Configuration.Class</code> to
 * <code>se.trillian.goodies.stripes.spring.SpringRuntimeConfiguration</code>:
 * 
 * <code>
 *  &lt;filter&gt;  
 *   &lt;init-param&gt;
 *       &lt;param-name&gt;Configuration.Class&lt;/param-name&gt;
 *       &lt;param-value&gt;se.trillian.goodies.stripes.spring.SpringRuntimeConfiguration&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 * &lt;/filter&gt;
 * </code>
 * <p>
 * For example can now a custom <code>ActionResolver</code> implementation be
 * placed in the Spring web application context and it will automatically be
 * autowired to the this class and used in the Stripes configuration.
 * </p>
 * 
 * @author Henric Müller
 * @version $Id$
 */
public class SpringRuntimeConfiguration extends RuntimeConfiguration {

    private ActionResolver actionResolver;

    private ActionBeanPropertyBinder actionBeanPropertyBinder;

    private ActionBeanContextFactory actionBeanContextFactory;

    private TypeConverterFactory typeConverterFactory;

    private LocalizationBundleFactory localizationBundleFactory;

    private LocalePicker localePicker;

    private FormatterFactory formatterFactory;

    private TagErrorRendererFactory tagErrorRendererFactory;

    private PopulationStrategy populationStrategy;

    private Map<LifecycleStage, Collection<Interceptor>> interceptors;

    private ExceptionHandler exceptionHandler;

    private MultipartWrapperFactory multipartWrapperFactory;

    /**
     * Autowires all properties for this configuration by retrieving the Spring
     * web application context and calls {@link DefaultConfiguration#init()}.
     */
    @Override
    public void init() {
        WebApplicationContext springContext = WebApplicationContextUtils
                .getRequiredWebApplicationContext(getServletContext());
        AutowireCapableBeanFactory beanFactory = springContext
                .getAutowireCapableBeanFactory();
        beanFactory.autowireBeanProperties(this,
                AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);

        super.init();
    }

    @Override
    protected ActionBeanContextFactory initActionBeanContextFactory() {
        return actionBeanContextFactory == null ? super.initActionBeanContextFactory()
                : initializeComponent(actionBeanContextFactory);
    }

    @Override
    protected ActionBeanPropertyBinder initActionBeanPropertyBinder() {
        return actionBeanPropertyBinder;
    }

    @Override
    protected ActionResolver initActionResolver() {
        return actionResolver == null ? super.initActionResolver()
                : initializeComponent(actionResolver);
    }

    @Override
    protected ExceptionHandler initExceptionHandler() {
        return exceptionHandler == null ? super.initExceptionHandler()
                : initializeComponent(exceptionHandler);
    }

    @Override
    protected FormatterFactory initFormatterFactory() {
        return formatterFactory == null ? super.initFormatterFactory()
                : initializeComponent(formatterFactory);
    }

    @Override
    protected Map<LifecycleStage, Collection<Interceptor>> initInterceptors() {
        if (interceptors == null) {
            return super.initInterceptors();
        }
        for (LifecycleStage s : interceptors.keySet()) {
            for (Interceptor i : interceptors.get(s)) {
                if (i instanceof ConfigurableComponent) {
                    initializeComponent((ConfigurableComponent) i);
                }
            }
        }
        return interceptors;
    }

    @Override
    protected LocalePicker initLocalePicker() {
        return localePicker == null ? super.initLocalePicker() 
                : initializeComponent(localePicker);
    }

    @Override
    protected LocalizationBundleFactory initLocalizationBundleFactory() {
        return localizationBundleFactory == null ? super.initLocalizationBundleFactory() 
                : initializeComponent(localizationBundleFactory);
    }

    @Override
    protected MultipartWrapperFactory initMultipartWrapperFactory() {
        return multipartWrapperFactory == null ? super.initMultipartWrapperFactory() 
                : initializeComponent(multipartWrapperFactory);
    }

    @Override
    protected PopulationStrategy initPopulationStrategy() {
        return populationStrategy == null ? super.initPopulationStrategy()
                : initializeComponent(populationStrategy);
    }

    @Override
    protected TagErrorRendererFactory initTagErrorRendererFactory() {
        return tagErrorRendererFactory == null ? super.initTagErrorRendererFactory() 
                : initializeComponent(tagErrorRendererFactory);
    }

    @Override
    protected TypeConverterFactory initTypeConverterFactory() {
        return typeConverterFactory == null ? super.initTypeConverterFactory()
                : initializeComponent(typeConverterFactory);
    }

    public void setActionBeanContextFactory(
            ActionBeanContextFactory actionBeanContextFactory) {
        this.actionBeanContextFactory = actionBeanContextFactory;
    }

    public void setActionBeanPropertyBinder(
            ActionBeanPropertyBinder actionBeanPropertyBinder) {
        this.actionBeanPropertyBinder = actionBeanPropertyBinder;
    }

    public void setActionResolver(ActionResolver actionResolver) {
        this.actionResolver = actionResolver;
    }

    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public void setFormatterFactory(FormatterFactory formatterFactory) {
        this.formatterFactory = formatterFactory;
    }

    public void setInterceptors(
            Map<LifecycleStage, Collection<Interceptor>> interceptors) {
        this.interceptors = interceptors;
    }

    public void setLocalePicker(LocalePicker localePicker) {
        this.localePicker = localePicker;
    }

    public void setLocalizationBundleFactory(
            LocalizationBundleFactory localizationBundleFactory) {
        this.localizationBundleFactory = localizationBundleFactory;
    }

    public void setMultipartWrapperFactory(
            MultipartWrapperFactory multipartWrapperFactory) {
        this.multipartWrapperFactory = multipartWrapperFactory;
    }

    public void setPopulationStrategy(PopulationStrategy populationStrategy) {
        this.populationStrategy = populationStrategy;
    }

    public void setTagErrorRendererFactory(
            TagErrorRendererFactory tagErrorRendererFactory) {
        this.tagErrorRendererFactory = tagErrorRendererFactory;
    }

    public void setTypeConverterFactory(
            TypeConverterFactory typeConverterFactory) {
        this.typeConverterFactory = typeConverterFactory;
    }

    protected <T extends ConfigurableComponent> T initializeComponent(T component) {
        if (component == null) {
            return null;
        }
        try {
            component.init(this);
            return component;
        } catch (Exception e) {
            throw new StripesRuntimeException(
                    "Could not init component of type ["
                            + component.getClass()
                            + "]. Please check "
                            + "the configuration parameters specified in your web.xml.",
                    e);
        }
    }

}
