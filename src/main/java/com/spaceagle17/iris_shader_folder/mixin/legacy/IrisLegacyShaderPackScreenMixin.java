package com.spaceagle17.iris_shader_folder.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Pseudo
@Debug(export = true)
@Mixin(targets = "net.coderbot.iris.gui.screen.ShaderPackScreen", remap = false)
public class IrisLegacyShaderPackScreenMixin extends Screen {
    @Shadow private Optional<Component> hoveredElementCommentTitle;
    @Shadow private List<FormattedCharSequence> hoveredElementCommentBody;
    @Shadow private int hoveredElementCommentTimer;

    protected IrisLegacyShaderPackScreenMixin(Component component) {
        super(component);
    }

    @Unique
    public void setShaderPackComment(Component title, Component body) {
        this.hoveredElementCommentTitle = Optional.of(title);
        this.hoveredElementCommentBody = new ArrayList<>(this.font.split(body, 306)); // 314 - 8 = 306
        this.hoveredElementCommentTimer = 21;
    }
}