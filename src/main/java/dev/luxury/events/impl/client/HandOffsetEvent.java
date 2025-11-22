package dev.luxury.events.impl.client;

import dev.luxury.events.impl.eventapi.events.Event;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
@Setter
public class HandOffsetEvent implements Event {
    MatrixStack matrices;
    ItemStack stack;
    Hand hand;
}
