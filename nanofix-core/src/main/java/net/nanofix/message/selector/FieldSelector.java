package net.nanofix.message.selector;

import net.nanofix.field.RawField;
import net.nanofix.message.FIXMessage;
import net.nanofix.field.Field;
import sun.plugin2.liveconnect.ArgumentHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * User: Mark
 * Date: 31/03/12
 * Time: 11:38
 */
public class FieldSelector implements MessageSelector {

    private final Field field;
    private static final byte[] ANY_FIELD_VALUE = new byte[] { -1 };

    public FieldSelector(Field field) {
        if (field == null) {
            throw new IllegalArgumentException("Argument 'field' is null");
        }
        this.field = field;
    }

    @Override
    public boolean isSelected(FIXMessage msg) {
        Field foundField = msg.getField(field.getTag());
        return ANY_FIELD_VALUE == field.getValue()
                || foundField != null && field.equals(foundField);
    }

    public static Field getTagOnlyField(int tag) {
        return new RawField(tag, ANY_FIELD_VALUE);
    }
}
