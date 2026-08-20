package com.grafie.botjava.persistence;

import com.grafie.botjava.service.LuaRoleStatusStore;
import jakarta.persistence.EntityManager;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "bot.mongodb.enabled=true",
        "jx3api.enabled=false",
        "bot.qq.message-ingress-mode=HOOK",
        "tx.bot.health-check-enabled=false"
})
@EnabledIfEnvironmentVariable(named = "BOT_MONGODB_URI", matches = ".+")
class DualDatabaseLiveSmokeIT {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MongoTemplate botMongoTemplate;

    @Autowired
    private LuaRoleStatusStore roleStatusStore;

    @Test
    @Transactional(readOnly = true)
    void shouldReadPostgresAndImportedMongoRolesInOneApplicationContext() {
        Number postgresResult = (Number) entityManager.createNativeQuery("select 1").getSingleResult();
        assertThat(postgresResult.intValue()).isEqualTo(1);

        Document ping = botMongoTemplate.getDb().runCommand(new Document("ping", 1));
        assertThat(ping.getDouble("ok")).isEqualTo(1.0d);
        assertThat(botMongoTemplate.getCollection("roles").countDocuments()).isEqualTo(384);

        List<Document> matches = roleStatusStore.findByServerAndRoleName("乾坤一掷", "醉卧平沙");
        assertThat(matches).isNotEmpty();
        assertThat(matches.getFirst())
                .containsEntry("服务器", "乾坤一掷")
                .containsEntry("角色名", "醉卧平沙")
                .containsKeys("每日签到任务", "角色金币", "精力", "侠行点");
    }
}
