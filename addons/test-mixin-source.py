"""Regression for the nested rarity mixins that caused false CI failures."""
import importlib.util
from pathlib import Path
import unittest

spec=importlib.util.spec_from_file_location('mixin_source',Path(__file__).with_name('mixin-source.py'))
scanner=importlib.util.module_from_spec(spec);spec.loader.exec_module(scanner)

class MixinSourceTests(unittest.TestCase):
    def test_all_real_rarity_mixins_are_scoped(self):
        source=Path(__file__).with_name('extras')/'RarityOutlineMixin.java'
        blocks=scanner.mixin_blocks(source.read_text(encoding='utf-8'))
        self.assertEqual(len(blocks),3)
        self.assertIn('GuiGraphicsExtractor.class',blocks[0][0])
        self.assertIn('GuiItemRenderState.class',blocks[1][0])
        self.assertIn('GuiRenderer.class',blocks[2][0])
        self.assertIn('holyLoisOutlineStart',blocks[0][1])
        self.assertNotIn('holyLoisTag',blocks[0][1])
        self.assertNotIn('holyLoisOutlineStart',blocks[1][1])
        self.assertIn('holyLoisOutline(',blocks[2][1])
    def test_string_and_comment_braces_do_not_end_a_class(self):
        source='@Mixin(A.class) public abstract static class One { String x="}"; /* } */ @Inject(method="one") private void one() {} } @Mixin(B.class) class Two { void two() {} }'
        blocks=scanner.mixin_blocks(source)
        self.assertEqual(len(blocks),2)
        self.assertIn('void one()',blocks[0][1])
        self.assertNotIn('void two()',blocks[0][1])
    def test_comment_examples_are_not_mixin_declarations(self):
        self.assertEqual(scanner.mixin_blocks('/* @Mixin(A.class) class Fake {} */'),[])
    def test_unclosed_body_is_rejected(self):
        with self.assertRaises(ValueError):scanner.mixin_blocks('@Mixin(A.class) class Broken {')

if __name__=='__main__':unittest.main()
