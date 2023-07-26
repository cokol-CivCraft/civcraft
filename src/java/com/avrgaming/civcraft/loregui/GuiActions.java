package com.avrgaming.civcraft.loregui;

import java.util.function.Function;

public enum GuiActions {
    BuildWithDefaultPersonalTemplate(BuildWithDefaultPersonalTemplate::new),
    BuildWithPersonalTemplate(BuildWithPersonalTemplate::new),
    BuildWithTemplate(BuildWithTemplate::new),
    OpenInventory(OpenInventory::new),
    ShowRecipe(ShowRecipe::new),
    SpawnItem(SpawnItem::new),
    TutorialRecipe(TutorialRecipe::new);

    private final Function<GuiActions, GuiAction> aNew;

    GuiActions(Function<GuiActions, GuiAction> aNew) {
        this.aNew = aNew;
    }

    public GuiAction getNew() {
        return aNew.apply(this);
    }
}
