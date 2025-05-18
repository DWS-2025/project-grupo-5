package com.echoreviews.mapper;

import com.echoreviews.dto.ReviewDTO;
import com.echoreviews.model.Review;
import com.echoreviews.model.Album;
import com.echoreviews.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.IterableMapping;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.ast.Node; // Flexmark's Node
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    // Flexmark parser and renderer setup
    static final MutableDataSet FLEXMARK_OPTIONS = new MutableDataSet();
    static final Parser FLEXMARK_PARSER = Parser.builder(FLEXMARK_OPTIONS).build();
    static final HtmlRenderer FLEXMARK_RENDERER = HtmlRenderer.builder(FLEXMARK_OPTIONS).build();
    // OWASP HTML Sanitizer policy
    static final PolicyFactory SANITIZER_POLICY = Sanitizers.FORMATTING.and(Sanitizers.BLOCKS);

    @Named("toHTMLDTO")
    @Mapping(source = "album.id", target = "albumId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "album.title", target = "albumTitle")
    @Mapping(source = "album.imageUrl", target = "albumImageUrl")
    @Mapping(source = "user.imageUrl", target = "userImageUrl")
    @Mapping(source = "content", target = "content", qualifiedByName = "markdownToHtml")
    ReviewDTO toDTO(Review review);

    @Named("toMarkdownDTO")
    @Mapping(source = "album.id", target = "albumId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "album.title", target = "albumTitle")
    @Mapping(source = "album.imageUrl", target = "albumImageUrl")
    @Mapping(source = "user.imageUrl", target = "userImageUrl")
    ReviewDTO toDTOWithMarkdown(Review review);

    @Mapping(target = "album", source = "albumId", qualifiedByName = "idToAlbum")
    @Mapping(target = "user", source = "userId", qualifiedByName = "idToUser")
    Review toEntity(ReviewDTO reviewDTO);

    @IterableMapping(qualifiedByName = "toHTMLDTO")
    List<ReviewDTO> toDTOList(List<Review> reviews);

    @IterableMapping(qualifiedByName = "toMarkdownDTO")
    List<ReviewDTO> toDTOListWithMarkdown(List<Review> reviews);

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

    @Named("idToAlbum")
    default Album idToAlbum(Long id) {
        if (id == null) return null;
        Album album = new Album();
        album.setId(id);
        return album;
    }

    @Named("idToUser")
    default User idToUser(Long id) {
        if (id == null) return null;
        User user = new User();
        user.setId(id);
        return user;
    }
}