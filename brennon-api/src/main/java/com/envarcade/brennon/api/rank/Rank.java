package com.envarcade.brennon.api.rank;

import net.kyori.adventure.text.Component;
import java.util.Set;

public interface Rank {

    String getId();

    String getDisplayName();

    Component getPrefix();

    Component getSuffix();

    int getWeight();

    Set<String> getPermissions();

    Set<String> getInheritance();

    boolean hasPermission(String permission);

    boolean isDefault();

    boolean isStaff();
}
