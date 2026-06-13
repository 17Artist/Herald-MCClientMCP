package ai.herald.clientmod.fabric.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(com.mojang.blaze3d.platform.Window.class)
public abstract class WindowMixin {

    @Inject(method = "<init>", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"))
    private void herald$beforeCreateWindow(CallbackInfo ci) {
        if ("true".equals(System.getProperty("herald.headless"))) {
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        }
    }
}
