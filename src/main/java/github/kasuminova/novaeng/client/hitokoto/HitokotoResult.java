package github.kasuminova.novaeng.client.hitokoto;

public record HitokotoResult(int id,
                             String uuid,
                             String hitokoto,
                             String type,
                             String from,
                             String fromWho,
                             String creator,
                             int creatorUid,
                             int reviewer,
                             String commitFrom,
                             String createdAt,
                             int length) {
}
