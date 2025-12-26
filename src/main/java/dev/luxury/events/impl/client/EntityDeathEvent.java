package dev.luxury.events.impl.client;

import dev.luxury.events.impl.eventapi.events.Event;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
@Getter
public class EntityDeathEvent implements Event {
    private final Entity entity;
    private final DamageSource source;

    public EntityDeathEvent(Entity entity, DamageSource source) {
        this.entity = entity;
        this.source = source;
    }
}