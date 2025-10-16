package com.pkg.openai.api.common;

import com.fasterxml.jackson.annotation.JsonValue;


public sealed class ImageModelParams {

    public static final class Dalle2Options extends ImageModelParams {

        public static final ImageModel MODEL = ImageModel.DALL_E_2;
        public static final ImageSize _256 = ImageSize._256_256;
        public static final ImageSize _512 = ImageSize._512_512;
        public static final ImageSize _1024 = ImageSize._1024_1024;
    }

    public static final class Dalle3Options extends ImageModelParams {

        public static final ImageModel MODEL = ImageModel.DALL_E_3;
        public static final ImageQuality STANDARD_QUALITY = ImageQuality.STANDARD;
        public static final ImageQuality HD_QUALITY = ImageQuality.HD;
        public static final ImageSize _1024 = ImageSize._1024_1024;
        public static final ImageSize LANDSCAPE = ImageSize._1792_1024;
        public static final ImageSize PORTRAIT = ImageSize._1024_1792;
    }

    public static final class GptImage1Options extends ImageModelParams {

        public static final ImageModel MODEL = ImageModel.GPT_IMAGE_1;
        public static final ImageQuality HIGH_QUALITY = ImageQuality.HIGH;
        public static final ImageQuality MEDIUM_QUALITY = ImageQuality.MEDIUM;
        public static final ImageQuality LOW_QUALITY = ImageQuality.LOW;
        public static final ImageSize _1024 = ImageSize._1024_1024;
        public static final ImageSize LANDSCAPE = ImageSize._1536_1024;
        public static final ImageSize PORTRAIT = ImageSize._1024_1536;

        public enum BackgroundOption {

            TRANSPARENT(false),
            OPAQUE(true),
            ;

            private final boolean value;

            BackgroundOption(boolean value) {
                this.value = value;
            }

            @JsonValue
            public boolean isValue() {
                return value;
            }
        }

        public enum Style {

            VIVID("vivid"),
            NATURAL("natural"),
            ;

            public final String value;

            Style(String value) {
                this.value = value;
            }

            @JsonValue
            public String getValue() {
                return value;
            }
        }
    }
}
