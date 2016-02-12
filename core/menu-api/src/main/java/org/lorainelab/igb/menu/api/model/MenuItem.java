package org.lorainelab.igb.menu.api.model;

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
 * @module.info context-menu-api
 */
public class MenuItem {

    private final String menuLabel;
    private MenuIcon menuIcon;
    private Function<Void, Void> action;
    private Set<MenuItem> subMenuItems;
    private int weight = 0;
    private boolean isEnabled;
    private Character mnemonic;

    public MenuItem(String menuLabel, Set<MenuItem> subMenuItems) {
        checkNotNull(menuLabel);
        checkNotNull(subMenuItems);
        checkState(!Strings.isNullOrEmpty(menuLabel));
        checkState(!subMenuItems.isEmpty());
        this.menuLabel = menuLabel;
        this.subMenuItems = ImmutableSet.copyOf(subMenuItems);
        isEnabled = true;
    }

    public MenuItem(String menuLabel, Function<Void, Void> action) {
        checkNotNull(menuLabel);
        checkState(!Strings.isNullOrEmpty(menuLabel));
        checkNotNull(action);
        this.menuLabel = menuLabel;
        this.action = action;
        subMenuItems = ImmutableSet.of();
        isEnabled = true;
    }

    public String getMenuLabel() {
        return menuLabel;
    }

    public Function<Void, Void> getAction() {
        return action;
    }

    public Set<MenuItem> getSubMenuItems() {
        return subMenuItems;
    }

    /**
     * Weight
     * =====
     *
     * The weight property specifies the sorting of MenuItems.
     * A greater weight is always below an element with a lower weight.
     *
     */
    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public void setMenuIcon(MenuIcon menuIcon) {
        this.menuIcon = menuIcon;
    }

    /**
     * MenuIcon
     * =====
     *
     * The weight property specifies the sorting of MenuItems.
     * A greater weight is always below of an element with a lower weight.
     *
     */
    public Optional<MenuIcon> getMenuIcon() {
        return Optional.ofNullable(menuIcon);
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public Optional<Character> getMnemonic() {
        return Optional.ofNullable(mnemonic);
    }

    public void setMnemonic(Character c) {
        this.mnemonic = c;
    }

}
