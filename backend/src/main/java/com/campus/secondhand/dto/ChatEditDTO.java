package com.campus.secondhand.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatEditDTO {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "消息过长")
    private String content;
}
