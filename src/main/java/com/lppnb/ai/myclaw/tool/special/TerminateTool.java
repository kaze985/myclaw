package com.lppnb.ai.myclaw.tool.special;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @author kaze
 * @date 2026/4/10 11:42
 */
@Slf4j
@Component
public class TerminateTool {
    @Tool(description = """
            Terminate the interaction when the request is met OR if the assistant cannot proceed further with the task.
            "When you have finished all the tasks, call this tool to end the work.
            """)
    public String doTerminate() {
        return "mission complete";
    }
}
