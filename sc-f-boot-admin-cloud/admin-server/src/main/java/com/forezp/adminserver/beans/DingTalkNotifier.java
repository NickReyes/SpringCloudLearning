package com.forezp.adminserver.beans;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.notify.AbstractStatusChangeNotifier;
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
    private String title = "监控报警、业务报警";
    private Expression message;

    public DingTalkNotifier(InstanceRepository repository) {
        super(repository);
        this.message = this.parser.parseExpression("*#{instance.registration.name}* (#{instance.id}) is *#{event.statusInfo.status}*", ParserContext.TEMPLATE_EXPRESSION);
    }

    @Override
    protected Mono<Void> doNotify(InstanceEvent instanceEvent, Instance instance) {
        if (webhookToken == null) {
            return Mono.error(new IllegalStateException("'webhookToken' must not be null."));
        }
        return Mono.fromRunnable(
                () -> restTemplate.postForEntity(webhookToken, createMessage(instanceEvent, instance), Void.class));
    }

    private HttpEntity<Map<String, Object>> createMessage(InstanceEvent event, Instance instance) {
        Map<String, Object> messageJson = new HashMap<>();
        HashMap<String, String> params = new HashMap<>();
        params.put("text", this.getMessage(event, instance));
        params.put("content", this.title);
        //设置需要艾特的人
        Map<String, Object> atJson = new HashMap<>();
        atJson.put("isAtAll", this.isAtAll);
        atJson.put("atMobiles", getAtMobilesString(this.atMobiles));

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

    private String getMessage(InstanceEvent event, Instance instance) {
        Map<String, Object> root = new HashMap<>();
        root.put("event", event);
        root.put("instance", instance);
        root.put("lastStatus", getLastStatus(event.getInstance()));
        StandardEvaluationContext context = new StandardEvaluationContext(root);
        context.addPropertyAccessor(new MapAccessor());
        return this.message.getValue(context, String.class);
    }

    public boolean isAtAll() {
        return isAtAll;
    }

    public void setAtAll(boolean atAll) {
        isAtAll = atAll;
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