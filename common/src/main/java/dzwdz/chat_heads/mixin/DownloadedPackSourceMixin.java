package dzwdz.chat_heads.mixin;

import dzwdz.chat_heads.ChatHeads;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.resources.server.PackReloadConfig;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(DownloadedPackSource.class)
public abstract class DownloadedPackSourceMixin {
    @Inject(method = "loadRequestedPacks", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void chatheads$checkForDisableResource(List<PackReloadConfig.IdAndPath> list, CallbackInfoReturnable<List<Pack>> cir, List<Pack> serverPacks) {
        for (Pack serverPack : serverPacks) {
            try (PackResources resources = serverPack.open()) {
                if (resources.getResource(PackType.CLIENT_RESOURCES, ChatHeads.DISABLE_RESOURCE) != null) {
                    ChatHeads.serverDisabledChatHeads = true;
                    ChatHeads.LOGGER.info("Chat Heads disabled by server request");
                }
            }
        }
    }
}
