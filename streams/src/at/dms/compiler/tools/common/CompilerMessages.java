
/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: CompilerMessages.java,v 1.3 2006-01-25 17:00:56 thies Exp $
 */

// Generated by msggen from CompilerMessages.msg
package at.dms.compiler.tools.common;


public interface CompilerMessages {
    MessageDescription  FORMATTED_ERROR = new MessageDescription("{0}", null, 0);
    MessageDescription  FORMATTED_CAUTION = new MessageDescription("{0}", null, 1);
    MessageDescription  FORMATTED_WARNING = new MessageDescription("{0}", null, 2);
    MessageDescription  FORMATTED_NOTICE = new MessageDescription("{0}", null, 3);
    MessageDescription  FORMATTED_INFO = new MessageDescription("{0}", null, 4);
    MessageDescription  NO_INPUT_FILE = new MessageDescription("No input file given", null, 0);
    MessageDescription  FILE_NOT_FOUND = new MessageDescription("File \"{0}\" not found", null, 0);
    MessageDescription  IO_EXCEPTION = new MessageDescription("I/O Exception on file \"{0}\": {1}", null, 0);
    MessageDescription  UNSUPPORTED_ENCODING = new MessageDescription("Character encoding \"{0}\" is not supported on this platform", null, 0);
    MessageDescription  CANNOT_CREATE = new MessageDescription("Cannot create file \"{0}\"", null, 0);
    MessageDescription  INVALID_LIST_FILE = new MessageDescription("Invalid list file \"{0}\" : {1}", null, 0);
    MessageDescription  NO_VIABLE_ALT_FOR_CHAR = new MessageDescription("Unexpected char \"{0}\"", null, 0);
    MessageDescription  UNEXPECTED_EOF = new MessageDescription("Unexpected end of file", null, 0);
    MessageDescription  EOF_IN_TRADITIONAL_COMMENT = new MessageDescription("End of file in comment", null, 0);
    MessageDescription  EOF_IN_ENDOFLINE_COMMENT = new MessageDescription("End of file in comment", null, 1);
    MessageDescription  ILLEGAL_CHAR = new MessageDescription("Unexpected char \"{0}\"", null, 0);
    MessageDescription  BAD_ESCAPE_SEQUENCE = new MessageDescription("Illegal escape sequence \"{0}\"", null, 0);
    MessageDescription  BAD_END_OF_LINE = new MessageDescription("Unexpected end of line in {0}", null, 0);
    MessageDescription  SYNTAX_ERROR = new MessageDescription("Syntax error: {0}", null, 0);
    MessageDescription  COMPILATION_STARTED = new MessageDescription("[ start compilation in verbose mode ]", null, 4);
    MessageDescription  FILE_PARSED = new MessageDescription("[ parsed {0} in {1} ms ]", null, 4);
    MessageDescription  INTERFACES_CHECKED = new MessageDescription("[ checked interfaces in {0} ms ]", null, 4);
    MessageDescription  BODY_CHECKED = new MessageDescription("[ checked body of {0} in {1} ms ]", null, 4);
    MessageDescription  CLASSFILE_GENERATED = new MessageDescription("[ optimized and generated {0} in {1} ms ]", null, 4);
    MessageDescription  JAVA_CODE_GENERATED = new MessageDescription("[ generated {0} ]", null, 4);
    MessageDescription  CLASS_LOADED = new MessageDescription("[ loaded {0} ]", null, 4);
    MessageDescription  COMPILATION_ENDED = new MessageDescription("[ compilation ended ]", null, 4);
}
