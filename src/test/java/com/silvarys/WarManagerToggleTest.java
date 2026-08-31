package com.silvarys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WarManagerToggleTest {

    @Test
    public void warSystemCanBeToggledOffAndOn() {
        WarManager.setWarSystemEnabled(true);
        Assertions.assertTrue(WarManager.isWarSystemEnabled());

        WarManager.setWarSystemEnabled(false);
        Assertions.assertFalse(WarManager.isWarSystemEnabled());

        WarManager.setWarSystemEnabled(true);
        Assertions.assertTrue(WarManager.isWarSystemEnabled());
    }
}
