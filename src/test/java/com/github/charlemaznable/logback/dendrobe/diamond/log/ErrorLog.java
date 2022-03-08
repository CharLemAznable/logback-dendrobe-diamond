package com.github.charlemaznable.logback.dendrobe.diamond.log;

import com.github.charlemaznable.logback.dendrobe.EqlLogBean;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqlLogBean("error")
public class ErrorLog {

    private String logId;
    private String logContent;
}
