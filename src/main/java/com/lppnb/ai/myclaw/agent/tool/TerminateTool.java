package com.lppnb.ai.myclaw.agent.tool;

import org.springframework.ai.tool.annotation.Tool;

/**
 * @author kaze
 * @date 2026/4/10 11:42
 */
public class TerminateTool {
    @Tool(description = """
            Terminate the interaction when the request is met OR if the assistant cannot proceed further with the task.
            "When you have finished all the tasks, call this tool to end the work.
            """)
    public String doTerminate() {
        return "mission complete";
    }
}
