package net.calvuz.qdue.domain.common.enums;

import androidx.annotation.NonNull;

/**
 * Available patterns for assignment creation.
 */
public enum Pattern
{
        QUATTRODUE( "quattrodue", "QuattroDue Standard Pattern" ),
        CUSTOM( "custom", "Custom User Pattern" );

        private final String key;
        private final String displayName;

        Pattern(String key, String displayName) {
            this.key = key;
            this.displayName = displayName;
        }

        @NonNull
        public String getKey() {
            return key;
        }

        @NonNull
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Check if this pattern requires team selection.
         */
        public boolean requiresTeamSelection() {
            return this == QUATTRODUE;
        }

        /**
         * Check if this pattern requires custom pattern selection.
         */
        public boolean requiresCustomPatternSelection() {
            return this == CUSTOM;
        }
}
