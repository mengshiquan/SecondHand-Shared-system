package com.campus.secondhand.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatSendDTO {

    @NotNull(message = "接收者不能为空")
    private Long receiverId;

    /** 关联商品（可选） */
    private Long productId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "消息过长")
    private String content;
}
