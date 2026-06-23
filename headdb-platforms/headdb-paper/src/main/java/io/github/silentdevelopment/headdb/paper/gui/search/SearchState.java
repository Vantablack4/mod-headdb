package io.github.silentdevelopment.headdb.paper.gui.search;

import io.github.silentdevelopment.grafik.gui.GuiContext;
import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class SearchState {

    private static final String REQUEST_STATE_KEY = "headdb.search.request";
    private static final String PAGE_STATE_KEY = "headdb.search.page";

    private SearchState() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull SearchRequest request(@NotNull GuiContext<SearchMenuState> context) {
        Objects.requireNonNull(context, "context");

        SearchRequest request = context.state().get(REQUEST_STATE_KEY, SearchRequest.class);

        if (request != null) {
            return request;
        }

        return context.source().request();
    }

    public static void request(@NotNull GuiContext<SearchMenuState> context, @NotNull SearchRequest request) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(request, "request");

        context.state().put(REQUEST_STATE_KEY, request.withPage(1));
        page(context, 1);
    }

    public static int page(@NotNull GuiContext<SearchMenuState> context) {
        Objects.requireNonNull(context, "context");

        Integer page = context.state().get(PAGE_STATE_KEY, Integer.class);

        if (page == null || page < 1) {
            return Math.max(1, request(context).page());
        }

        return page;
    }

    public static void page(@NotNull GuiContext<SearchMenuState> context, int page) {
        Objects.requireNonNull(context, "context");
        context.state().put(PAGE_STATE_KEY, Math.max(1, page));
    }
}