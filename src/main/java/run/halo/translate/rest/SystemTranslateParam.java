package run.halo.translate.rest;

import lombok.Data;


@Data
public class SystemTranslateParam {


    private String text;

    /**
     * 译语种
     */
    private String toLan;

}
