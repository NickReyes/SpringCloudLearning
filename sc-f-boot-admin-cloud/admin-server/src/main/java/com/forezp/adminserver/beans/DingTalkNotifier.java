package com.forezp.adminserver.beans;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent;
import de.codecentric.boot.admin.server.notify.AbstractStatusChangeNotifier;
import org.json.simple.JSONObject;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * @Author Nick
 * @Date 2020/9/5 13:11
 * @Version 1.0
 */
public class DingTalkNotifier extends AbstractStatusChangeNotifier {
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private RestTemplate restTemplate = new RestTemplate();
    private String webhookToken;
    private String atMobiles;
    private boolean isAtAll;
    private String msgtype = "text";
    private String title = "系统警告";
    private Expression message;

    public DingTalkNotifier(InstanceRepository repository) {
        super(repository);
        //this.message = this.parser.parseExpression("#{instance.registration.serviceUrl}.#{event.statusInfo.status}", ParserContext.TEMPLATE_EXPRESSION);
    }

    @Override
    protected Mono<Void> doNotify(InstanceEvent instanceEvent, Instance instance) {
        if (webhookToken == null) {
            return Mono.error(new IllegalStateException("'webhookToken' must not be null."));
        }
        Mono<Void> objectMono = Mono.fromRunnable(
                () -> restTemplate.postForEntity(webhookToken, createMessage(instance), Void.class));
        return objectMono;
    }

    private HttpEntity<Map<String, Object>> createMessage(Instance instance) {
        Map<String, Object> messageJson = new HashMap<>();
        HashMap<String, String> params = new HashMap<>();
        params.put("content", title + " : \n" + getMessage(instance) + "\n");
        //设置需要艾特的人
        Map<String, Object> atJson = new HashMap<>();
        atJson.put("isAtAll", this.isAtAll);
        if (!this.isAtAll) {
            atJson.put("atMobiles", getAtMobilesString(this.atMobiles));
        }
        messageJson.put("at", atJson);
        messageJson.put("msgtype", this.msgtype);
        messageJson.put(this.msgtype, params);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        return new HttpEntity<>(messageJson, headers);
    }

    private List<String> getAtMobilesString(String s) {
        List<String> mobileList = new ArrayList<>();
        String[] mobiles = s.split(",");
        mobileList.addAll(Arrays.asList(mobiles));
        return mobileList;
    }

    private String getMessage(Instance instance) {
//        Map<String, Object> root = new HashMap<>();
//        root.put("event", event);
//        root.put("instance", instance);
//        root.put("lastStatus", getLastStatus(event.getInstance()));
//        StandardEvaluationContext context = new StandardEvaluationContext(root);
//        context.addPropertyAccessor(new MapAccessor());
//        return this.message.getValue(context, String.class);
        //使用自定义的模板
        String serviceName = instance.getRegistration().getName();
        String serviceUrl = instance.getRegistration().getServiceUrl();
        String status = instance.getStatusInfo().getStatus();
        Map<String, Object> details = instance.getStatusInfo().getDetails();
        return "【服务名】: " + serviceName +
                "\n【服务地址】: " + serviceUrl +
                "\n【状态】: " + status +
                "\n【详情】: " + JSONObject.toJSONString(details);
    }

    public boolean getIsAtAll() {
        return isAtAll;
    }

    public void setIsAtAll(boolean isAtAll) {
        this.isAtAll = isAtAll;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getWebhookToken() {
        return webhookToken;
    }

    public void setWebhookToken(String webhookToken) {
        this.webhookToken = webhookToken;
    }

    public String getAtMobiles() {
        return atMobiles;
    }

    public void setAtMobiles(String atMobiles) {
        this.atMobiles = atMobiles;
    }

    public String getMsgtype() {
        return msgtype;
    }

    public void setMsgtype(String msgtype) {
        this.msgtype = msgtype;
    }

    public Expression getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = this.parser.parseExpression(message, ParserContext.TEMPLATE_EXPRESSION);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}