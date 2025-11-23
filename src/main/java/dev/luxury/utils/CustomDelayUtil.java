package dev.luxury.utils;

import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeListSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CustomDelayUtil {
    private long lastRightClickTime = 0;
    private long lastSpaceTime = 0;
    
    /**
     * Функция customdelay - применяет задержку на выбранные кнопки
     * @param customDelay ModeListSetting для выбора на какие кнопки применять задержку
     * @param rightClickDelay SliderSetting для задержки правой кнопки мыши (в миллисекундах)
     * @param spaceDelay SliderSetting для задержки пробела (в миллисекундах)
     * @param buttonType тип кнопки: "rightClick" для правой кнопки мыши, "space" для пробела
     * @return true если задержка прошла и действие можно выполнить, false если нужно подождать
     */
    public boolean customdelay(ModeListSetting customDelay, SliderSetting rightClickDelay, SliderSetting spaceDelay, String buttonType) {
        if (customDelay == null) return true;
        
        long currentTime = System.currentTimeMillis();
        
        if (buttonType.equals("rightClick")) {
            BooleanSetting rightClickSetting = customDelay.getValueByName("Правая кнопка мыши");
            if (rightClickSetting != null && rightClickSetting.get()) {
                long delay = (long) rightClickDelay.getValue();
                if (currentTime - lastRightClickTime < delay) {
                    return false;
                }
                lastRightClickTime = currentTime;
            }
        } else if (buttonType.equals("space")) {
            BooleanSetting spaceSetting = customDelay.getValueByName("Пробел");
            if (spaceSetting != null && spaceSetting.get()) {
                long delay = (long) spaceDelay.getValue();
                if (currentTime - lastSpaceTime < delay) {
                    return false;
                }
                lastSpaceTime = currentTime;
            }
        }
        
        return true;
    }
    
    /**
     * Сброс таймеров задержки
     */
    public void reset() {
        lastRightClickTime = 0;
        lastSpaceTime = 0;
    }
}

