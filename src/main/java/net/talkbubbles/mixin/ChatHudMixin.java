package net.talkbubbles.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextVisitFactory;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.talkbubbles.TalkBubbles;
import net.talkbubbles.accessor.OtherClientPlayerEntityAccessor;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Shadow
    @Final
    @Mutable
    private MinecraftClient client;

    // onChatMessage is now done in MessageHandler.class
    @Inject(method = "Lnet/minecraft/client/gui/hud/ChatHud;addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"))
    private void addMessageMixin(Text message, @Nullable MessageSignatureData signature, @Nullable MessageIndicator indicator, CallbackInfo info) {
        if (extractSender(message) != null) {
            List<OtherClientPlayerEntity> list = client.world.getEntitiesByClass(OtherClientPlayerEntity.class, client.player.getBoundingBox().expand(TalkBubbles.CONFIG.chatRange),
                    EntityPredicates.EXCEPT_SPECTATOR);
            for (int i = 0; i < list.size(); i++)
                if (list.get(i).getUuid().equals(extractSender(message))) {
                    String stringMessage = message.getString();
                    stringMessage = stringMessage.replace("<" + StringUtils.substringBetween(stringMessage, "<", ">") + "> ", "");
                    String[] string = stringMessage.split(" ");
                    List<String> stringList = new ArrayList<>();
                    String stringCollector = "";

                    int width = 0;
                    int height = 0;
                    for (int u = 0; u < string.length; u++) {
                        if (client.textRenderer.getWidth(stringCollector) < TalkBubbles.CONFIG.maxChatWidth
                                && client.textRenderer.getWidth(stringCollector) + client.textRenderer.getWidth(string[u]) <= TalkBubbles.CONFIG.maxChatWidth) {
                            stringCollector = stringCollector + " " + string[u];
                            if (u == string.length - 1) {
                                stringList.add(stringCollector);
                                height++;
                                if (width < client.textRenderer.getWidth(stringCollector))
                                    width = client.textRenderer.getWidth(stringCollector);
                            }
                        } else {
                            stringList.add(stringCollector);

                            height++;
                            if (width < client.textRenderer.getWidth(stringCollector))
                                width = client.textRenderer.getWidth(stringCollector);

                            stringCollector = string[u];

                            if (u == string.length - 1) {
                                stringList.add(stringCollector);
                                height++;
                                if (width < client.textRenderer.getWidth(stringCollector))
                                    width = client.textRenderer.getWidth(stringCollector);
                            }
                        }
                    }

                    if (width % 2 != 0)
                        width++;

                    ((OtherClientPlayerEntityAccessor) list.get(i)).setChatText(stringList, list.get(i).age, width, height);
                    break;
                }
        }

    }

    private UUID extractSender(Text text) {
        String string = TextVisitFactory.removeFormattingCodes(text);
        println(string);
        String string2 = StringUtils.substringBetween(string, "<", ">");
        println(string2);
        if (string2 == null) {
            return Util.NIL_UUID;
        }
        return this.client.getSocialInteractionsManager().getUuid(string2);
    }
}
