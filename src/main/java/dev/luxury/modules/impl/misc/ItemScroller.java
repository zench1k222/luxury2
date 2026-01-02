package dev.luxury.modules.impl.misc;


import dev.luxury.events.impl.client.ClickSlotEvent;
import dev.luxury.events.impl.client.HandledScreenEvent;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.math.StopWatch;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

import java.util.stream.Stream;

@ModuleAnnotation(
        name = "ItemScroller",
        desc = "",
        category = Category.Misc
)
public class ItemScroller extends Module {
    static MinecraftClient mc = MinecraftClient.getInstance();

        StopWatch stopWatch = new StopWatch();

        SliderSetting scrollerSetting = new SliderSetting("Скорость",50,0,200,1f);

        public ItemScroller() {
            addSettings(scrollerSetting);
        }

        @EventTarget
        public void onHandledScreen(HandledScreenEvent e) {
            Slot hoverSlot = e.getSlotHover();
            SlotActionType actionType = isKey(mc.options.dropKey) ? SlotActionType.THROW : isKey(mc.options.attackKey) ? SlotActionType.QUICK_MOVE : null;

            if (isKey(mc.options.sneakKey) && !isKey(mc.options.sprintKey) && hoverSlot != null && hoverSlot.hasStack() && actionType != null && stopWatch.every(scrollerSetting.getFloatValue())) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, hoverSlot.id, actionType.equals(SlotActionType.THROW) ? 1 : 0, actionType, mc.player);
            }
        }

        @EventTarget
        public void onClickSlot(ClickSlotEvent e) {
            int slotId = e.getSlotId();
            if (slotId < 0 || slotId > mc.player.currentScreenHandler.slots.size()) return;
            Slot slot = mc.player.currentScreenHandler.getSlot(slotId);
            Item item = slot.getStack().getItem();

            if (item != null && isKey(mc.options.sneakKey) && isKey(mc.options.sprintKey) && stopWatch.every(50)) {
                slots().filter(s -> s.getStack().getItem().equals(item) && s.inventory.equals(slot.inventory)).forEach(s -> mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, s.id, 1, e.getActionType(), mc.player));
            }
    }
    public static Stream<Slot> slots() {
        return mc.player.currentScreenHandler.slots.stream();
    }

    public boolean isKey(KeyBinding key) {
        return isKey(key.getDefaultKey().getCategory(), key.getDefaultKey().getCode());
    }

    public boolean isKey(InputUtil.Type type, int keyCode) {
        if (keyCode != -1) switch (type) {
            case InputUtil.Type.KEYSYM: return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == 1;
            case InputUtil.Type.MOUSE: return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode) == 1;
        }
        return false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
