package com.studyclass.content;

import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageProcessClass extends MessageProcessAbstract {

    @Autowired
    MqMessageService mqMessageService;

    @Override
    public boolean execute(MqMessage mqMessage) {
        // 1. 从获取任务id
        Long id = mqMessage.getId();
        log.debug("开始执行任务：{}", id);
        // 2. 获取1阶段状态
//        MqMessageService mqMessageService = this.getMqMessageService();
        int stageOne = mqMessageService.getStageOne(id);
        if (stageOne == 0) {
            log.debug("开始执行第一阶段的任务");
            // TODO 第一阶段任务逻辑
            // 一阶段任务完成，此方法的逻辑是将stageOne设置为1
            int i = mqMessageService.completedStageOne(1);
            if (i == 1) {
                log.debug("完成一阶段任务");
            }
        } else {
            log.debug("一阶段任务已经完成，无需再次执行");
        }
        return true;
    }
}