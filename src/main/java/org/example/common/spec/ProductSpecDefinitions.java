package org.example.common.spec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProductSpecDefinitions {

    private ProductSpecDefinitions() {
    }

    public static final class SpecField {
        private final String key;
        private final String label;
        private final List<String> options;

        public SpecField(String key, String label, List<String> options) {
            this.key = key;
            this.label = label;
            this.options = options;
        }

        public String key() {
            return key;
        }

        public String label() {
            return label;
        }

        public List<String> options() {
            return options;
        }
    }

    private static final LinkedHashMap<String, List<SpecField>> DEFINITIONS = new LinkedHashMap<>();

    static {
        DEFINITIONS.put("Mouse", List.of(
                field("dpi", "DPI", "8000", "12000", "16000", "26000"),
                field("sensor", "Sensor", "Optical", "PWM3395", "HERO 2"),
                field("weight_g", "Weight (g)", "58", "63", "69", "74"),
                field("connection", "Connection", "Wired", "Wireless", "Hybrid"),
                field("buttons", "Buttons", "5", "6", "7", "8")
        ));

        DEFINITIONS.put("Klavye", List.of(
                field("switch_type", "Switch Type", "Red", "Brown", "Blue", "Optical"),
                field("layout", "Layout", "TKL", "75%", "96%", "Full Size"),
                field("key_count", "Key Count", "84", "87", "96", "104"),
                field("backlight", "Backlight", "RGB", "White", "None"),
                field("connection", "Connection", "USB-C", "Wireless", "Hybrid")
        ));

        DEFINITIONS.put("Kulaklik", List.of(
                field("driver_mm", "Driver (mm)", "40", "50", "53"),
                field("impedance_ohm", "Impedance (ohm)", "32", "45", "60"),
                field("microphone", "Microphone", "Integrated", "Detachable", "Noise Cancelling"),
                field("connection", "Connection", "3.5mm", "USB", "Wireless"),
                field("weight_g", "Weight (g)", "240", "280", "320")
        ));

        DEFINITIONS.put("Ekran", List.of(
                field("inch", "Screen Size (inch)", "24", "27", "31.5", "34"),
                field("refresh_hz", "Refresh Rate (Hz)", "144", "165", "240", "360"),
                field("panel", "Panel Type", "IPS", "VA", "OLED"),
                field("resolution", "Resolution", "1920x1080", "2560x1440", "3440x1440", "3840x2160"),
                field("weight_kg", "Weight (kg)", "3.6", "4.8", "6.2", "7.5")
        ));

        DEFINITIONS.put("Kasa", List.of(
                field("form_factor", "Form Factor", "Mini ITX", "Mid Tower", "Full Tower"),
                field("motherboard_support", "Motherboard Support", "ATX", "mATX", "Mini-ITX", "E-ATX"),
                field("fan_slots", "Fan Slots", "4", "6", "8", "10"),
                field("max_gpu_mm", "Max GPU Length (mm)", "320", "360", "400"),
                field("material", "Material", "Mesh + Steel", "Aluminum + Tempered", "Steel + Tempered")
        ));

        DEFINITIONS.put("Kasa Icerikleri", List.of(
                field("ram_gb", "RAM (GB)", "16", "32", "64"),
                field("ssd_gb", "SSD (GB)", "512", "1000", "2000"),
                field("cpu_model", "CPU", "Ryzen 5 7600", "Ryzen 7 7800X3D", "Core i7 14700K"),
                field("gpu_model", "GPU", "RTX 4070 Super", "RTX 4080", "RX 7900 XT"),
                field("psu_w", "PSU (W)", "650", "750", "850", "1000")
        ));
    }

    private static SpecField field(String key, String label, String... options) {
        return new SpecField(key, label, List.of(options));
    }

    public static Map<String, List<SpecField>> all() {
        LinkedHashMap<String, List<SpecField>> copy = new LinkedHashMap<>();
        DEFINITIONS.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        return copy;
    }

    public static List<SpecField> fieldsForCategory(String categoryName) {
        List<SpecField> fields = DEFINITIONS.get(categoryName);
        if (fields == null) {
            return List.of();
        }
        return List.copyOf(fields);
    }

    public static Map<String, String> sanitizeForCategory(String categoryName, Map<String, String> rawSpecs) {
        LinkedHashMap<String, String> sanitized = new LinkedHashMap<>();
        if (rawSpecs == null || rawSpecs.isEmpty()) {
            return sanitized;
        }
        for (SpecField field : fieldsForCategory(categoryName)) {
            String value = rawSpecs.get(field.key());
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            sanitized.put(field.key(), trimmed);
        }
        return sanitized;
    }

    public static Map<String, String> sampleSpecsForCategory(String categoryName, int seedIndex) {
        List<SpecField> fields = fieldsForCategory(categoryName);
        LinkedHashMap<String, String> specs = new LinkedHashMap<>();
        int safeIndex = Math.max(0, seedIndex);
        for (SpecField field : fields) {
            List<String> options = field.options();
            if (options == null || options.isEmpty()) {
                continue;
            }
            specs.put(field.key(), options.get(safeIndex % options.size()));
        }
        return specs;
    }

    public static List<String> categoryNames() {
        return new ArrayList<>(DEFINITIONS.keySet());
    }
}
