package com.mattmakesapps.mostrx;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

public class DrugResponse implements Serializable {
    public Map<String, Brand> variations;

    public Brand getDefaultBrand() {
        for (Brand b : variations.values()) {
            if (b.isDefault) {
                return b;
            }
        }

        for (Brand b : variations.values()) {
            if (b.isGeneric) {
                return b;
            }
        }

        for (Brand b : variations.values()) {
            return b;
        }

        return null;
    }

    private static <K, V> int getFlattenedIndex(Map<K, V> map, V element) {
        return new ArrayList<>(map.values()).indexOf(element);
    }

    public int getDefaultBrandIndex() {
        return getFlattenedIndex(variations, getDefaultBrand());
    }

    public String getBrandName() {
        for (Brand v : variations.values()) {
            if (!v.isGeneric) {
                return v.name;
            }
        }

        for (Brand v : variations.values()) {
            return v.name;
        }

        return null;
    }

    public String getGenericName() {
        for (Brand v : variations.values()) {
            if (v.isGeneric) {
                return v.name;
            }
        }

        for (Brand v : variations.values()) {
            return v.name;
        }

        return null;
    }

    public static class Brand implements Serializable {
        public Map<String, Form> forms;
        public boolean isDefault;
        public boolean isGeneric;
        public String label;
        public String name;

        public Form getDefaultForm() {
            for (Form f : forms.values()) {
                if (f.isDefault) {
                    return f;
                }
            }

            for (Form f : forms.values()) {
                return f;
            }

            return null;
        }

        public int getDefaultFormIndex() {
            return getFlattenedIndex(forms, getDefaultForm());
        }

        @NotNull
        public String toString() {
            return label;
        }
    }

    public static class Form implements Serializable {
        public boolean isDefault;
        public String name;
        public Map<String, Strength> strengths;

        public Strength getDefaultStrength() {
            for (Strength s : strengths.values()) {
                if (s.isDefault) {
                    return s;
                }
            }

            for (Strength s : strengths.values()) {
                return s;
            }

            return null;
        }

        public int getDefaultStrengthIndex() {
            return getFlattenedIndex(strengths, getDefaultStrength());
        }

        @NotNull
        public String toString() {
            return name;
        }
    }

    public static class Strength implements Serializable {
        public boolean isDefault;
        public Map<String, Quantity> quantities;
        public String strength;

        public Quantity getDefaultQuantity() {
            for (Quantity q : quantities.values()) {
                if (q.isDefault) {
                    return q;
                }
            }

            for (Quantity q : quantities.values()) {
                return q;
            }

            return null;
        }

        public int getDefaultQuantityIndex() {
            return getFlattenedIndex(quantities, getDefaultQuantity());
        }

        @NotNull
        public String toString() {
            return strength;
        }
    }

    public static class Quantity implements Serializable {
        public int amt;
        public boolean isDefault;
        public String ndc;

        @NotNull
        public String toString() {
            return "" + amt;
        }
    }
}
