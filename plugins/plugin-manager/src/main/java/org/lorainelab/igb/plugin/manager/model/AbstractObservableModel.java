/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.plugin.manager.model;

import java.lang.reflect.Field;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeckstei
 */
public abstract class AbstractObservableModel<T> implements Observable, Comparable<T> {

    private static org.slf4j.Logger alogger;

    public AbstractObservableModel() {
        alogger = LoggerFactory.getLogger(this.getClass());
    }


    @Override
    public void addListener(InvalidationListener listener) {
        try {
            for (Field field : this.getClass().getDeclaredFields()) {
                if (Observable.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    ((Observable) field.get(this)).addListener(listener);
                    field.setAccessible(false);
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            alogger.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        try {
            for (Field field : this.getClass().getDeclaredFields()) {
                if (Observable.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    ((Observable) field.get(this)).removeListener(listener);
                    field.setAccessible(false);
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            alogger.error(ex.getMessage(), ex);
        }
    }
}
