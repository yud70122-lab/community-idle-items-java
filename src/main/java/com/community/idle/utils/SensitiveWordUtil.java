package com.community.idle.utils;

import java.util.*;

public class SensitiveWordUtil {

    private static final Set<String> SENSITIVE_WORDS = new HashSet<>();
    private static final Set<String> SEVERE_SENSITIVE_WORDS = new HashSet<>();

    static {
        SENSITIVE_WORDS.addAll(Arrays.asList(
                "色情", "赌博", "毒品", "枪支", "暴力", "恐怖",
                "诈骗", "传销", "走私", "假货", "高仿", "山寨",
                "盗版", "侵权", "违法", "违禁", "管制", "枪支",
                "弹药", "爆炸物", "剧毒", "放射性", "麻醉品",
                "精神药品", "处方药", "医疗器械", "办证", "刻章",
                "发票", "套现", "洗钱", "外汇", "虚拟币", "比特币",
                "代考", "代刷", "代练", "外挂", "私服", "破解版",
                "成人", "性用品", "催情", "迷奸", "强奸", "变态",
                "侮辱", "诽谤", "恐吓", "威胁", "骚扰"
        ));

        SEVERE_SENSITIVE_WORDS.addAll(Arrays.asList(
                "枪支", "弹药", "毒品", "麻醉品", "精神药品",
                "爆炸物", "剧毒", "放射性", "色情", "淫秽",
                "赌博", "诈骗", "传销", "走私", "恐怖", "暴力"
        ));
    }

    public static SensitiveWordCheckResult checkSensitiveWords(String text) {
        SensitiveWordCheckResult result = new SensitiveWordCheckResult();
        result.setSafe(true);
        result.setSevere(false);
        result.setFoundWords(new ArrayList<>());

        if (text == null || text.isEmpty()) {
            return result;
        }

        String lowerText = text.toLowerCase();

        for (String word : SENSITIVE_WORDS) {
            if (lowerText.contains(word)) {
                result.setSafe(false);
                result.getFoundWords().add(word);
            }
        }

        for (String word : SEVERE_SENSITIVE_WORDS) {
            if (lowerText.contains(word)) {
                result.setSevere(true);
                break;
            }
        }

        return result;
    }

    public static SensitiveWordCheckResult checkItemContent(String title, String description) {
        StringBuilder content = new StringBuilder();
        if (title != null) {
            content.append(title).append(" ");
        }
        if (description != null) {
            content.append(description);
        }
        return checkSensitiveWords(content.toString());
    }

    public static class SensitiveWordCheckResult {
        private boolean safe;
        private boolean severe;
        private List<String> foundWords;

        public boolean isSafe() {
            return safe;
        }

        public void setSafe(boolean safe) {
            this.safe = safe;
        }

        public boolean isSevere() {
            return severe;
        }

        public void setSevere(boolean severe) {
            this.severe = severe;
        }

        public List<String> getFoundWords() {
            return foundWords;
        }

        public void setFoundWords(List<String> foundWords) {
            this.foundWords = foundWords;
        }
    }
}
