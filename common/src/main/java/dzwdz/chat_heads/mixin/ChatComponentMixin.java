package dzwdz.chat_heads.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dzwdz.chat_heads.ChatHeads;
import dzwdz.chat_heads.mixinterface.GuiMessageOwnerAccessor;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;

@Mixin(value = ChatComponent.class, priority = 990) // apply before Quark's ChatComponentMixin
public abstract class ChatComponentMixin {
    @ModifyVariable(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/GuiMessage$Line;addedTime()I"
            ),
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;III)V"
    )
    public GuiMessage.Line chatheads$captureGuiMessage(GuiMessage.Line guiMessage,
            @Share("guiMessage") LocalRef<GuiMessage.Line> guiMessageRef, @Share("chatOffset") LocalIntRef chatOffsetRef) {
        guiMessageRef.set(guiMessage);
        chatOffsetRef.set(ChatHeads.getChatOffset(guiMessage));
        return guiMessage;
    }

    @ModifyArg(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Font;drawShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/util/FormattedCharSequence;FFI)I",
                    ordinal = 0
            ),
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;III)V",
            index = 2
    )
    public float chatheads$moveText(PoseStack poseStack, FormattedCharSequence formattedCharSequence, float x, float y, int color,
            @Share("chatOffset") LocalIntRef chatOffsetRef, @Share("y") LocalIntRef yRef, @Share("opacity") LocalFloatRef opacityRef) {
        yRef.set((int) y);
        opacityRef.set((((color >> 24) + 256) % 256) / 255f); // haha yes
        return chatOffsetRef.get();
    }

    @ModifyExpressionValue(method = "getTagIconLeft(Lnet/minecraft/client/GuiMessage$Line;)I", at = @At(value = "CONSTANT", args = "intValue=4"))
    private int chatheads$moveTagIcon(int four, @Share("chatOffset") LocalIntRef chatOffsetRef) {
        return four + chatOffsetRef.get();
    }

    @Inject(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Font;drawShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/util/FormattedCharSequence;FFI)I",
                    ordinal = 0
            ),
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;III)V"
    )
    public void chatheads$renderChatHead(PoseStack matrixStack, int i, int j, int k, CallbackInfo ci,
            @Share("guiMessage") LocalRef<GuiMessage.Line> guiMessage, @Share("y") LocalIntRef yRef, @Share("opacity") LocalFloatRef opacityRef) {
        PlayerInfo owner = ChatHeads.getOwner(guiMessage.get());
        if (owner != null) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, opacityRef.get());
            ChatHeads.renderChatHead(matrixStack, 0, yRef.get(), owner);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
    }

    @ModifyVariable(
            at = @At("STORE"),
            method = "getClickedComponentStyleAt(DD)Lnet/minecraft/network/chat/Style;"
    )
    public GuiMessage.Line chatheads$updateChatOffset(GuiMessage.Line guiMessage, @Share("chatOffset") LocalIntRef chatOffsetRef) {
        chatOffsetRef.set(ChatHeads.getChatOffset(guiMessage));
        return guiMessage;
    }

    @ModifyArg(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/StringSplitter;componentStyleAtWidth(Lnet/minecraft/util/FormattedCharSequence;I)Lnet/minecraft/network/chat/Style;"
            ),
            method = "getClickedComponentStyleAt(DD)Lnet/minecraft/network/chat/Style;",
            index = 1
    )
    public int chatheads$correctClickPosition(int x, @Share("chatOffset") LocalIntRef chatOffsetRef) {
        return x - chatOffsetRef.get();
    }

    @ModifyExpressionValue(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;getWidth()I"
            ),
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V"
    )
    public int chatheads$fixTextOverflow(int original) {
        // at this point, neither guiMessage nor chatOffset are well-defined
        return original - ChatHeads.getChatOffset(ChatHeads.getLineOwner());
    }

    // Compact Chat calls this at the beginning of addMessage (to get rid of old duplicate messages)
    @Inject(
            method = "refreshTrimmedMessage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void chatheads$transferMessageOwner(CallbackInfo ci, int i, GuiMessage guiMessage) {
        // transfer owner from GuiMessage to new GuiMessage.Line
        ChatHeads.refreshingLineOwner = ChatHeads.getOwner(guiMessage);
    }
}
