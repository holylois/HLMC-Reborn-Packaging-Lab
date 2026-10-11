"""Small source scanner for top-level and nested mixin declarations (preserves offsets)."""
import re


def mixin_blocks(text):
    # Braces inside comments and strings do not close a class. Keep offsets identical to the original source.
    masked=re.sub(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'',
                  lambda m:' '*(m.end()-m.start()),text,flags=re.S)
    declarations=re.finditer(r'@Mixin\s*\((.*?)\)\s*(?:(?:public|protected|private|abstract|static|final|sealed|non-sealed)\s+)*(?:class|interface)\s+([\w$]+)',text,re.S)
    blocks=[]
    for declaration in declarations:
        if masked[declaration.start()]!='@':continue
        start=masked.find('{',declaration.end())
        if start<0:raise ValueError('Mixin class has no body: '+declaration.group(2))
        depth=1;end=start+1
        while end<len(masked) and depth:
            if masked[end]=='{':depth+=1
            elif masked[end]=='}':depth-=1
            end+=1
        if depth:raise ValueError('Unclosed mixin class: '+declaration.group(2))
        blocks.append((declaration.group(1),text[start+1:end-1]))
    return blocks
