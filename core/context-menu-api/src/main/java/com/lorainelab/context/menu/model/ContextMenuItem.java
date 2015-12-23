/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.context.menu.model;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 *
 * @author dcnorris
 */
public class ContextMenuItem {

    private final String menuLabel;

    private MenuIcon menuIcon;

    private Function<Void, Void> action;

    private Set<ContextMenuItem> subMenuItems;

    private int weight = 0;

    public ContextMenuItem(String menuLabel, Set<ContextMenuItem> subMenuItems) {
        checkNotNull(menuLabel);
        checkNotNull(subMenuItems);
        checkState(!Strings.isNullOrEmpty(menuLabel));
        checkState(!subMenuItems.isEmpty());
        this.menuLabel = menuLabel;
        this.subMenuItems = ImmutableSet.copyOf(subMenuItems);
    }

    public ContextMenuItem(String menuLabel, Function<Void, Void> action) {
        checkNotNull(menuLabel);
        checkState(!Strings.isNullOrEmpty(menuLabel));
        checkNotNull(action);
        this.menuLabel = menuLabel;
        this.action = action;
        subMenuItems = ImmutableSet.of();
    }

    public String getMenuLabel() {
        return menuLabel;
    }

    public Function<Void, Void> getAction() {
        return action;
    }

    public Set<ContextMenuItem> getSubMenuItems() {
        return subMenuItems;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public void setMenuIcon(MenuIcon menuIcon) {
        this.menuIcon = menuIcon;
    }

    public Optional<MenuIcon> getMenuIcon() {
        return Optional.ofNullable(menuIcon);
    }



}
