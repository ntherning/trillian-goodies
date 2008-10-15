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
package se.trillian.goodies.stripes.localization;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.sourceforge.stripes.config.Configuration;
import net.sourceforge.stripes.localization.LocalizationBundleFactory;

import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceResourceBundle;

/**
 * A Stripes {@link LocalizationBundleFactory} that can be configured using 
 * Spring {@link LocalizationBundleFactory} as error message bundle and 
 * form field bundle.
 *  
 * @author Henric Müller
 * @version $Id$
 */
public class MessageSourceLocalizationBundleFactory implements LocalizationBundleFactory {

    private MessageSource errorMessageSource;
    private MessageSource formFieldMessageSource;
    
    public void setErrorMessageSource(MessageSource errorMessageSource) {
        this.errorMessageSource = errorMessageSource;
    }

    public void setFormFieldMessageSource(MessageSource formFieldMessageSource) {
        this.formFieldMessageSource = formFieldMessageSource;
    }

    public ResourceBundle getErrorMessageBundle(Locale locale)
            throws MissingResourceException {
        return new MessageSourceResourceBundle(errorMessageSource, locale);
    }

    public ResourceBundle getFormFieldBundle(Locale locale)
            throws MissingResourceException {
        return new MessageSourceResourceBundle(formFieldMessageSource, locale);
    }

    public void init(Configuration configuration) throws Exception {
        
    }

}
