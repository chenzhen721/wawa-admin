package com.ttpod.star.admin.job

import com.ttpod.rest.anno.Rest
import com.ttpod.star.admin.BaseController
import com.ttpod.star.admin.lab.CatchuController
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

/**
 * 定时任务调用
 */
@Rest
class JobController extends BaseController {
    static final Logger logger = LoggerFactory.getLogger(JobController.class)

    @Resource
    CatchuController catchuController

    def job_push_order(HttpServletRequest req) {
        return catchuController.push_order(req)
    }
}
