package github.kasuminova.mmce.common.util;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DataReference<T> {

    private T value;

    public DataReference(final T value) {
        this.value = value;
    }

}
