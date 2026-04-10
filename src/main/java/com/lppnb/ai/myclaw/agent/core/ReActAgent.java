package com.lppnb.ai.myclaw.agent.core;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * @author kaze
 * @date 2026/4/10 14:06
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent{
    /**
     *  思考
     * @return
     */
    protected abstract boolean think();

    /**
     *  行动
     * @return
     */
    protected abstract String act();

    /**
     *  下一步
     * @return
     */
    @Override
    protected String step() {
        boolean shouldAct = think();
        if(!shouldAct){
            return "think step finished, no need to act.";
        }
        return act();
    }
}
