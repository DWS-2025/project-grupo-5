package com.echoreviews.mapper;

import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.model.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.ast.Node; // Flexmark's Node
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper { // Changed back to interface

    // Flexmark parser and renderer setup
    static final MutableDataSet FLEXMARK_OPTIONS = new MutableDataSet();
    static final Parser FLEXMARK_PARSER = Parser.builder(FLEXMARK_OPTIONS).build();
    static final HtmlRenderer FLEXMARK_RENDERER = HtmlRenderer.builder(FLEXMARK_OPTIONS).build();
    // OWASP HTML Sanitizer policy
    static final PolicyFactory SANITIZER_POLICY = Sanitizers.FORMATTING.and(Sanitizers.BLOCKS);

    @Mapping(source = "album.id", target = "albumId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "album.title", target = "albumTitle")
    @Mapping(source = "album.imageUrl", target = "albumImageUrl")
    @Mapping(source = "user.imageUrl", target = "userImageUrl")
    @Mapping(source = "content", target = "content", qualifiedByName = "markdownToHtml")
    ReviewDTO toDTO(Review review);

    // No changes needed for toEntity as it will take raw markdown from DTO
    Review toEntity(ReviewDTO reviewDTO);

    List<ReviewDTO> toDTOList(List<Review> reviews); // This will also use the toDTO method
    List<Review> toEntityList(List<ReviewDTO> reviewDTOs);

    @Named("markdownToHtml")
    default String markdownToSanitizedHtml(String markdown) {
        if (markdown == null) {
            return null;
        }
        Node document = FLEXMARK_PARSER.parse(markdown);
        String rawHtml = FLEXMARK_RENDERER.render(document);
        return SANITIZER_POLICY.sanitize(rawHtml);
    }
}