/*
 * Copyright (c) 2011 Khanh Tuong Maudoux <kmx.petals@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jetoile.jmanager4all.jmx;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cette classe permet d'encapsuler un proxy dynamique dans un MBean Dynamic.
 * 
 * @author khanh
 * 
 */
public class MBeanWrapper implements DynamicMBean {

    static final private Logger LOGGER = LoggerFactory.getLogger(MBeanWrapper.class);

    private final MBeanInfo mBeanInfo;
    private final Object proxy;

    public MBeanWrapper(final MBeanInfo mBeanInfo, final Object proxy) {
        this.mBeanInfo = mBeanInfo;
        this.proxy = proxy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#getAttribute(java.lang.String)
     */
    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        MBeanAttributeInfo[] mBeanAttributeInfos = mBeanInfo.getAttributes();
        for (MBeanAttributeInfo mBeanAttributeInfo : mBeanAttributeInfos) {
            if (StringUtils.equals(attribute, mBeanAttributeInfo.getName())) {
                if (mBeanAttributeInfo.isReadable()) {
                    return invokeGetter(attribute, mBeanAttributeInfo);
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    Object invokeGetter(String attribute, MBeanAttributeInfo mBeanAttributeInfo) {
        try {
            Method method = null;
            if (mBeanAttributeInfo.isIs()) {
                method = this.proxy.getClass().getMethod("is" + StringUtils.capitalize(attribute), new Class[0]);
                return method.invoke(this.proxy, new Object[0]);
            } else {
                method = this.proxy.getClass().getMethod("get" + StringUtils.capitalize(attribute), new Class[0]);
                return method.invoke(this.proxy, new Object[0]);
            }
        } catch (SecurityException e) {
            LOGGER.error("unable to get remote attribute info {}: {}", attribute, e);
        } catch (NoSuchMethodException e) {
            LOGGER.error("unable to get remote attribute info {}: {}", attribute, e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("unable to get remote attribute info {}: {}", attribute, e);
        } catch (IllegalAccessException e) {
            LOGGER.error("unable to get remote attribute info {}: {}", attribute, e);
        } catch (InvocationTargetException e) {
            LOGGER.error("unable to get remote attribute info {}: {}", attribute, e);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#setAttribute(javax.management.Attribute)
     */
    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        MBeanAttributeInfo[] mBeanAttributeInfos = mBeanInfo.getAttributes();
        for (MBeanAttributeInfo mBeanAttributeInfo : mBeanAttributeInfos) {
            if (StringUtils.equals(attribute.getName(), mBeanAttributeInfo.getName())) {
                if (mBeanAttributeInfo.isWritable()) {
                    invokeSetter(attribute, mBeanAttributeInfo);
                } else {
                    return;
                }
            }
        }
        return;
    }

    void invokeSetter(Attribute attribute, MBeanAttributeInfo mBeanAttributeInfo) {
        try {
            Method method = null;
            if (mBeanAttributeInfo.isIs()) {
                method = this.proxy.getClass().getMethod("set" + StringUtils.capitalize(attribute.getName()), boolean.class);
                method.invoke(this.proxy, attribute.getValue());
            } else {
                method = this.proxy.getClass().getMethod("set" + StringUtils.capitalize(attribute.getName()), attribute.getValue().getClass());
                method.invoke(this.proxy, attribute.getValue());
            }
        } catch (SecurityException e) {
            LOGGER.error("unable to set remote attribute info {}: {}", attribute, e);
        } catch (NoSuchMethodException e) {
            LOGGER.error("unable to set remote attribute info {}: {}", attribute, e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("unable to set remote attribute info {}: {}", attribute, e);
        } catch (IllegalAccessException e) {
            LOGGER.error("unable to set remote attribute info {}: {}", attribute, e);
        } catch (InvocationTargetException e) {
            LOGGER.error("unable to set remote attribute info {}: {}", attribute, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#getAttributes(java.lang.String[])
     */
    @Override
    public AttributeList getAttributes(String[] attributes) {
        AttributeList result = new AttributeList();
        for (String attribute : attributes) {
            Attribute currentAttribute;
            try {
                currentAttribute = new Attribute(attribute, getAttribute(attribute));
                result.add(currentAttribute);
            } catch (AttributeNotFoundException e) {
                LOGGER.error("unable to get remote attribute info {}: {}", attribute, e);
            } catch (MBeanException e) {
                LOGGER.error("unable to get remote attribute info {}: {}", attribute, e);
            } catch (ReflectionException e) {
                LOGGER.error("unable to get remote attribute info {}: {}", attribute, e);
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#setAttributes(javax.management.AttributeList )
     */
    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        AttributeList result = new AttributeList();
        for (int i = 0; i < attributes.size(); i++) {
            Attribute attribute = (Attribute) attributes.get(i);
            try {
                setAttribute(attribute);
                result.add(attribute);
            } catch (AttributeNotFoundException e) {
                LOGGER.error("unable to set remote attribute {}: {}", attribute.getName(), e);
            } catch (MBeanException e) {
                LOGGER.error("unable to set remote attribute {}: {}", attribute.getName(), e);
            } catch (ReflectionException e) {
                LOGGER.error("unable to set remote attribute {}: {}", attribute.getName(), e);
            } catch (InvalidAttributeValueException e) {
                LOGGER.error("unable to Set remote attribute {}: {}", attribute.getName(), e);
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#invoke(java.lang.String, java.lang.Object[], java.lang.String[])
     */
    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        Class[] paramTypes = null;
        if (signature != null) {
            paramTypes = new Class[signature.length];
            for (int i = 0; i < signature.length; ++i) {
                paramTypes[i] = signature[i].getClass();
            }
        }

        try {
            Method method = this.proxy.getClass().getMethod(actionName, paramTypes);
            return method.invoke(this.proxy, params);
        } catch (SecurityException e) {
            LOGGER.error("unable to invoke {}: {}", actionName, e);
        } catch (NoSuchMethodException e) {
            LOGGER.error("unable to invoke {}: {}", actionName, e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("unable to invoke {}: {}", actionName, e);
        } catch (IllegalAccessException e) {
            LOGGER.error("unable to invoke {}: {}", actionName, e);
        } catch (InvocationTargetException e) {
            LOGGER.error("unable to invoke {}: {}", actionName, e);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#getMBeanInfo()
     */
    @Override
    public MBeanInfo getMBeanInfo() {
        return this.mBeanInfo;
    }

}
