package github.kasuminova.novaeng.client.hitokoto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class HitokotoResult {
    private int id;
    private String UUID;
    private String hitokoto;
    private String type;
    private String from;
    private String fromWho;
    private String creator;
    private int creatorUid;
    private int reviewer;
    private String commitFrom;
    private String createdAt;
    private int length;
}