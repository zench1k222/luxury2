package dev.luxury.mixin.render.impl.client;

import dev.luxury.utils.managers.CommandManager;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(ChatInputSuggestor.class)
public class ChatInputSuggestorMixin {

    @Shadow @Final TextFieldWidget textField;

    @Shadow @Final private List<OrderedText> messages;

    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow private ChatInputSuggestor.SuggestionWindow window;

    @Shadow boolean completingSuggestions;

    @Inject(method = "refresh", at = @At("HEAD"), cancellable = true)
    private void preUpdateSuggestion(CallbackInfo ci) {
        String text = this.textField.getText().substring(0, Math.min(this.textField.getText().length(), this.textField.getCursor()));

        if (!text.startsWith(CommandManager.getInstance().getPrefix())) return;

        String[] completions = CommandManager.getInstance()
                .tabComplete(text)
                .toArray(String[]::new);

        if (completions.length == 0) return;

        ci.cancel();

        if (this.completingSuggestions) return;

        this.textField.setSuggestion(null);
        this.window = null;
        this.messages.clear();

        StringRange range = StringRange.between(text.lastIndexOf(" ") + 1, text.length());
        List<Suggestion> suggestionList = Stream.of(completions)
                .map(s -> new Suggestion(range, s))
                .collect(Collectors.toList());

        Suggestions suggestions = new Suggestions(range, suggestionList);
        this.pendingSuggestions = new CompletableFuture<>();
        this.pendingSuggestions.complete(suggestions);

        ((ChatInputSuggestor) (Object) this).show(true);
    }
}
