package nl.knaw.huc.di.elaborate.elabctl.archiver

import nl.knaw.huygens.tei.Comment
import nl.knaw.huygens.tei.CommentHandler
import nl.knaw.huygens.tei.Traversal
import nl.knaw.huygens.tei.XmlContext

class IgnoreCommentHandler : CommentHandler<XmlContext> {
    override fun visitComment(p0: Comment?, p1: XmlContext?): Traversal = Traversal.NEXT
}