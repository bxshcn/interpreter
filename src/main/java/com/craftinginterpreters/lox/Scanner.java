package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

class Scanner {
	private final String source;
	private final List<Token> tokens = new ArrayList<>();
	private int start = 0; // the first character in the lexeme being scanned
	private int current = 0; // the character currently being considered
	private int line = 1;

	public Scanner(String source) {
		this.source = source;
	}

	private static final Map<String, TokenType> keywords;

	static {
		keywords = new HashMap<>();
		keywords.put("and", AND);
		keywords.put("class", CLASS);
		keywords.put("else", ELSE);
		keywords.put("false", FALSE);
		keywords.put("for", FOR);
		keywords.put("fun", FUN);
		keywords.put("if", IF);
		keywords.put("nil", NIL);
		keywords.put("or", OR);
		keywords.put("print", PRINT);
		keywords.put("return", RETURN);
		keywords.put("super", SUPER);
		keywords.put("this", THIS);
		keywords.put("true", TRUE);
		keywords.put("var", VAR);
		keywords.put("while", WHILE);
	}

	List<Token> scanTokens() {
		while (!isAtEnd()) {
			// We are at the beginning of the next lexeme.
			start = current;
			scanToken();
		}

		tokens.add(new Token(EOF, "", null, line));
		return tokens;
	}

	private boolean isAtEnd() {
		return current >= source.length();
	}

	private void scanToken() {
		char c = advance();
		switch (c) {
			case '(':
				addToken(LEFT_PAREN);
				break;
			case ')':
				addToken(RIGHT_PAREN);
				break;
			case '{':
				addToken(LEFT_BRACE);
				break;
			case '}':
				addToken(RIGHT_BRACE);
				break;
			case ',':
				addToken(COMMA);
				break;
			case '.':
				addToken(DOT);
				break;
			case '-':
				addToken(MINUS);
				break;
			case '+':
				addToken(PLUS);
				break;
			case ';':
				addToken(SEMICOLON);
				break;
			case '*':
				addToken(STAR);
				break;
			case '!':
				addToken(match('=') ? BANG_EQUAL : BANG);
				break;
			case '=':
				addToken(match('=') ? EQUAL_EQUAL : EQUAL);
				break;
			case '<':
				addToken(match('=') ? LESS_EQUAL : LESS);
				break;
			case '>':
				addToken(match('=') ? GREATER_EQUAL : GREATER);
				break;
			case '/':
				if (match('/')) {
					// A comment goes until the end of the line.
					while (peek() != '\n' && !isAtEnd())
						advance();
				} else if (match('*')) {  // [*]support block comments
					blockComment();
				} else {
					addToken(SLASH);
				}
				break;
			case ' ':
			case '\r':
			case '\t':
				// Ignore whitespace.
				break;

			case '\n':
				line++;
				break;
			case '"':
				string();
				break;
			default:
				if (isDigit(c)) {
					number();
				} else if (isAlpha(c)) {
					identifier();
				} else {
					Lox.error(line, "Unexpected character.");
					break;
				}
		}
	}

	private void identifier() {
		// get lexeme
		while (isAlphaNumeric(peek()))
			advance();
		String text = source.substring(start, current);
		TokenType type = keywords.get(text);
		if (type == null)
			type = IDENTIFIER;
		addToken(type);
	}

	// for input
	private char advance() {
		// access and devour
		return source.charAt(current++);
	}

	// helper function
	private void addToken(TokenType type) {
		addToken(type, null);
	}

	// for output
	// if the type is Literals, we will compute them into an object
	// for single charater token, the object is just null. so we make
	// a simpler func as "addToken(TokenType type)" to omit the second parameter
	private void addToken(TokenType type, Object literal) {
		String text = source.substring(start, current);
		tokens.add(new Token(type, text, literal, line));
	}

	// this is just one character ahead, what about two or more characters ahead?
	private boolean match(char expected) {
		if (isAtEnd())
			return false;
		// access but not devour
		if (source.charAt(current) != expected)
			return false;

		// devour
		current++;
		return true;
	}

	// peek just access but not devour charset
	private char peek() {
		if (isAtEnd())
			return '\0';
		return source.charAt(current);
	}

	private void string() {
		while (peek() != '"' && !isAtEnd()) {
			if (peek() == '\n')
				line++;
			advance();
		}

		if (isAtEnd()) {
			Lox.error(line, "Unterminated string.");
			return;
		}

		// The closing ".
		advance();

		// Trim the surrounding quotes.
		String value = source.substring(start + 1, current - 1);
		addToken(STRING, value);
	}

	private void blockComment() {
		while (peek() != '*' && peekNext() != '/' && !isAtEnd()) {
			if (peek() == '\n')
				line++;
			advance();
		}

		if (isAtEnd()) {
			Lox.error(line, "Unterminated block comment.");
			return;
		}

		// the enclosing */.
		advance();
		advance();
	}

	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	private void number() {
		while (isDigit(peek()))
			advance();

		// Look for a fractional part.
		if (peek() == '.' && isDigit(peekNext())) {
			// Consume the "."
			advance();

			while (isDigit(peek()))
				advance();
		}

		addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
	}

	private char peekNext() {
		if (current + 1 >= source.length())
			return '\0';
		return source.charAt(current + 1);
	}

	private boolean isAlpha(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
	}

	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}
}
