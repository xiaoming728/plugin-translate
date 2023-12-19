package run.halo.translate.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemTranslateParam {


    private String text;

    /**
     * 译语种
     */
    private String toLan;

}
